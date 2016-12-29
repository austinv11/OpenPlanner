package com.austinv11.planner.core.db

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.table.DatabaseTable
import com.j256.ormlite.table.TableUtils

/**
 * This manages databases accessed by the server.
 */
object DatabaseManager {

    /**
     * This is the url leading to the database.
     */
    const val DATABASE_URL = "jdbc:h2:./database"
    
    val CONNECTION_SOURCE = JdbcConnectionSource(DATABASE_URL)
    
    val ACCOUNT_DAO: Dao<Account, Int> = DaoManager.createDao(CONNECTION_SOURCE, Account::class.java)
    
    init {
        TableUtils.createTableIfNotExists(CONNECTION_SOURCE, Account::class.java)
        Runtime.getRuntime().addShutdownHook(Thread({ CONNECTION_SOURCE.close() })) //TODO: Scale connection sources better
    }
    
    @DatabaseTable(tableName = "accounts")
    class Account {
        
        constructor()

        constructor(id: Int, username: String, email: String, hash: ByteArray, salt: ByteArray, verified: Boolean, plugins: Array<String>) {
            this.id = id
            this.username = username
            this.email = email
            this.hash = hash
            this.salt = salt
            this.verified = verified
            this.plugins = plugins

            //This constructor is only used by OpenPlanner, so its safe to immediately create in the DAO
            ACCOUNT_DAO.create(this)
        }


        companion object Columns {
            const val ID = "id"
            const val USERNAME = "user"
            const val EMAIL = "email"
            const val PASSWORD_HASH = "hash"
            const val PASSWORD_SALT = "salt"
            const val VERIFIED = "verified"
            const val PLUGINS = "plugins"
        }
        
        @DatabaseField(columnName = ID, generatedId = true)
        var id: Int = 0
        
        @DatabaseField(columnName = USERNAME, canBeNull = false)
        var username: String = ""
        
        @DatabaseField(columnName = EMAIL, canBeNull = false)
        var email: String = ""
        
        @DatabaseField(columnName = PASSWORD_HASH, canBeNull = false)
        var hash: ByteArray = byteArrayOf()
        
        @DatabaseField(columnName = PASSWORD_SALT, canBeNull = false)
        var salt: ByteArray = byteArrayOf()
        
        @DatabaseField(columnName = VERIFIED, canBeNull = false)
        var verified: Boolean = false
        
        @DatabaseField(columnName = PLUGINS, canBeNull = false)
        var plugins: Array<String> = arrayOf()
    }
}
