package com.austinv11.planner.core.scripting

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task

/**
 * This represents a process executed by a script.
 * @param toExecute The function representing the script process.
 */
class ScriptProcess(val toExecute: ScriptProcess.() -> Unit) {

    /**
     * This is a mutable map representing the environment variables exposed to the script.
     */
    val ENVIRONMENT_VARIABLES = mutableMapOf<String, Any>()
    /**
     * This returns true if the process is currently executing.
     */
    val isRunning: Boolean
        get() = process != null && !isFinished
    /**
     * This returns true if the process finished executing.
     */
    val isFinished: Boolean
        get() = _finished
    /**
     * This is called when the script finishes processing.
     */
    var callback: ((wasInterrupted: Boolean, exception: Exception?) -> Unit)? = null
    
    private var _finished = false
    private var process: Promise<Unit?, Exception?>? = null

    /**
     * This starts the process.
     */
    fun start() {
        process = task { toExecute() } success { callback?.invoke(false, null) } fail { callback?.invoke(true, it) }
        _finished = true
    }

    /**
     * This cancels the script execution.
     * @return Whether the script was executed or not.
     */
    fun cancel(): Boolean {
        if (isRunning) {
            process!!.context.stop(force = true, block = true)
            return true
        }
        
        return false
    }
}
