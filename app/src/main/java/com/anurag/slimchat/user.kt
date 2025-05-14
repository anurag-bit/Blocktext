package com.anurag.slimchat

class user {
    var name: String? = null
    var email: String? = null
    var uid: String? = null
    var profileImageUrl: String? = null
    var status: String? = null
    var lastSeen: Long? = null
    var isOnline: Boolean? = false

    constructor(){}

    constructor(name: String?, email: String?, uid: String?){
        this.name = name
        this.email = email
        this.uid = uid
    }
    
    constructor(name: String?, email: String?, uid: String?, profileImageUrl: String?, status: String?, lastSeen: Long?, isOnline: Boolean?){
        this.name = name
        this.email = email
        this.uid = uid
        this.profileImageUrl = profileImageUrl
        this.status = status
        this.lastSeen = lastSeen
        this.isOnline = isOnline
    }
}
