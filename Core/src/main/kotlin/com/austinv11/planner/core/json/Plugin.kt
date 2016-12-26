package com.austinv11.planner.core.json

import java.util.*

data class Repo(val metadata: RepoMetadata, val plugins: Array<String>) {
    
    override fun equals(other: Any?): Boolean{
        return this === other ||
                (other is Repo &&
                        Arrays.equals(other.plugins, plugins) &&
                        metadata == other.metadata)
    }

    override fun hashCode(): Int {
        return Objects.hash(metadata, Arrays.hashCode(plugins))
    }
}

data class RepoMetadata(val name: String, val icon: String, val description: String)

data class Plugin(val metadata: PluginMetadata, val init_script: String, val resources: Array<String>) {
    
    override fun equals(other: Any?): Boolean{
        return this === other || 
                (other is Plugin && 
                        Arrays.equals(other.resources, resources) && 
                        metadata == other.metadata && 
                        init_script == other.init_script)
    }

    override fun hashCode(): Int {
        return Objects.hash(metadata, init_script, Arrays.hashCode(resources))
    }
}

data class PluginMetadata(val name: String, val icon: String, val author: String, val version: String, val description: String)
