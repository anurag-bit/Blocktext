package com.anurag.slimchat

class message {
    var message: String? = null
    var senderId: String? = null
    var timestamp: Long? = null
    var isRead: Boolean? = false

    constructor(){}

    constructor(message: String?, senderId: String?) {
        this.message = message
        this.senderId = senderId
        this.timestamp = System.currentTimeMillis()
        this.isRead = false
    }
    
    constructor(message: String?, senderId: String?, timestamp: Long?, isRead: Boolean?) {
        this.message = message
        this.senderId = senderId
        this.timestamp = timestamp
        this.isRead = isRead
    }
}