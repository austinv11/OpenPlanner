package com.austinv11.planner.core.plugins

import com.austinv11.planner.core.json.Plugin
import com.austinv11.planner.core.json.Repo
import com.austinv11.planner.core.json.RepoMetadata
import com.austinv11.planner.core.plugins.LocalPluginRepository.REPO_DIR
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.util.*

/**
 * This represents a generic plugin repo.
 */
interface IPluginRepository {

    /**
     * This is the unix timestamp when this repo was last updated.
     */
    val lastUpdated: Long
    /**
     * This is the plugins which are contained in this repo.
     */
    val plugins: Array<Plugin>
    /**
     * This is the metadata about this repo.
     */
    val metadata: RepoMetadata

    /**
     * This is called to updated the plugin repository listings.
     */
    fun updateList()

    /**
     * This is used to find a plugin by its name.
     * @param name The (exact) plugin name.
     * @return The plugin (if found).
     */
    fun findPluginByName(name: String): Plugin? {
        try {
            return plugins.first { it.metadata.name == name }
        } catch (e: NoSuchElementException) {
            return null
        }
    }
}

/**
 * This represents the local cached plugin repository.
 */
object LocalPluginRepository : IPluginRepository {
    
    override val lastUpdated: Long
        get() = _lastUpdated
    override val plugins: Array<Plugin>
        get() {
            updateList()
            return _plugins.keys.toTypedArray()
        } 
    override val metadata: RepoMetadata = RepoMetadata("localhost", "", "The internal local repository.")

    val REPO_DIR = File("./plugins/")
    private var _lastUpdated: Long = -1
    private val _plugins = mutableMapOf<Plugin, File>()
    private val GSON = Gson()
    
    init {
        if (!REPO_DIR.exists())
            REPO_DIR.mkdir()
    }
    
    override fun updateList() {
        synchronized(_plugins) {
            _plugins.clear()
            REPO_DIR.listFiles { pathname -> pathname?.isDirectory ?: false }.forEach {
                it.listFiles { pathname -> pathname?.isDirectory ?: false }.forEach { //Each plugin directory has a subdirectory for each stored version 
                    val index = File(it, "index.json")
                    if (index.exists()) {
                        val plugin = GSON.fromJson(index.readText(), Plugin::class.java)
                        _plugins.put(plugin, it)
                    }
                }
            }
            
            _lastUpdated = System.currentTimeMillis()
        }
    }
}

/**
 * This represents a remote plugin repository.
 */
open class RemotePluginRepository(private val url: String) : IPluginRepository {
    
    override val lastUpdated: Long
        get() = _lastUpdated
    override val plugins: Array<Plugin>
        get() = _plugins.keys.toTypedArray()
    override val metadata: RepoMetadata
        get() = _repo.metadata
    
    private var _lastUpdated: Long = -1
    private val _plugins = mutableMapOf<Plugin, String>()
    private lateinit var _repo: Repo
    private val GSON = Gson()
    
    init {
        updateList()
    }
    
    override fun updateList() {
        synchronized(this) {
            _plugins.clear()
            
            val (request, response, result) = url.httpGet().responseString()
            result.fold({
                _repo = GSON.fromJson(it, Repo::class.java)
            },{
                throw IOException("Unable to retrieve repository listings for url: $url")
            })
            
            _repo.plugins.forEach { 
                val pluginUrl = normalizeUrl(url, it)

                val (request, response, result) = pluginUrl.httpGet().responseString()
                result.fold({
                    _plugins.put(GSON.fromJson(it, Plugin::class.java), pluginUrl.removeSuffix("index.json"))
                },{
                    throw IOException("Unable to retrieve plugin index for url: $pluginUrl")
                })
            }
            
            //All plugin listings retrieved
            
            _lastUpdated = System.currentTimeMillis()
        }
    }
    
    private fun normalizeUrl(parentUrl: String, startingUrl: String): String { //TODO: This is still prone to failure, let's hope in the meantime all plugin urls are  absolute
        var modifiedUrl: String = startingUrl
        
        if (!startingUrl.contains("://")) { //Missing protocol, assuming that this is a subdirectory of the original repo url
            val shouldAddForwardSlash = startingUrl.startsWith("/") or parentUrl.endsWith("/")
            val slash = if (shouldAddForwardSlash) "/" else ""
            modifiedUrl = (parentUrl + slash + startingUrl).replaceAfter("://", "//", "/")
        }
        
        if (!modifiedUrl.endsWith("/"))
            modifiedUrl += "/"
        
        if (!modifiedUrl.endsWith("index.json"))
            modifiedUrl += "index.json"
        
        return modifiedUrl
    }
    
    open fun downloadPlugin(plugin: Plugin) {
        if (!managesPlugin(plugin))
            throw IllegalArgumentException("Plugin $plugin is not hosted on this repo!")
        
        val pluginDir = File(REPO_DIR, "${plugin.metadata.name}/${plugin.metadata.version}")
        
        if (!pluginDir.exists()) { //Skip download if the plugin already exists
            pluginDir.mkdirs()
            
            File(pluginDir, "index.json").writeText(GSON.toJson(plugin)) //No need to download the index since it is already cached
            
            val baseUrl = _plugins[plugin]
            
            (plugin.resources + plugin.init_script).forEach { 
                val filepath = it.removePrefix(".").removePrefix("/")
                val (request, response, result) = (baseUrl + filepath).httpDownload().destination { response, url ->
                    val file = File(pluginDir, filepath)
                    file.mkdirs()
                    file.delete()
                    return@destination file
                }.responseString()
                result.fold({
                    //Success
                },{
                    throw IOException("Unable to download plugin resource from url ${request.url} (intended path: $filepath)")
                })
            }
            
            LocalPluginRepository.updateList()
        }
    }
    
    fun managesPlugin(plugin: Plugin): Boolean {
        return _plugins.containsKey(plugin)
    }
}


class CombinedRemotePluginRepository(private val initialRepos: Array<RemotePluginRepository>): RemotePluginRepository("") {
    
    val repos: Array<RemotePluginRepository>
        get() = _repos.toTypedArray()

    override val lastUpdated: Long
        get() = _lastUpdated
    override val plugins: Array<Plugin>
        get() {
            var _plugins = arrayOf<Plugin>()
            repos.forEach { _plugins += it.plugins }
            return _plugins
        }
    override val metadata = RepoMetadata("Combined Repository [internal]", "null", "An internal implementation of a plugin repository which manages multiple remote plugin repositories.")
    
    private val _repos = mutableListOf(*initialRepos)
    private var _lastUpdated: Long = -1

    override fun updateList() {
        repos.forEach { it.updateList() }
        _lastUpdated = System.currentTimeMillis()
    }
    
    override fun downloadPlugin(plugin: Plugin) {
        _repos.find { it.managesPlugin(plugin) }?.downloadPlugin(plugin) ?: throw IllegalArgumentException("Plugin $plugin could not be found on any managed repo!")
    }
    
    fun addRepo(repo: RemotePluginRepository) {
        _repos.add(repo)
    }
    
    fun removeRepo(repo: RemotePluginRepository) {
        _repos.remove(repo)
    }
}
