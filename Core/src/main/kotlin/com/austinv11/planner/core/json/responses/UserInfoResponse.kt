package com.austinv11.planner.core.json.responses

data class UserInfoResponse(var session: Long, var username: String, var email: String, var verified: Boolean, var plugins: Array<String>)
