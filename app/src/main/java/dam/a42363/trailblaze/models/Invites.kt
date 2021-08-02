package dam.a42363.trailblaze.models

class Invites {
    var idReceived: String? = null
    var idInvite: String? = null
    var nome: String? = null
    var photoUrl: String? = null
    var idRoute: String? = null
    var type: String? = null

    constructor() {}
    constructor(idInvite: String?, nome: String?, photoUrl: String?, idRoute: String?,idReceived: String?, type: String?) {
        this.idInvite = idInvite
        this.nome = nome
        this.photoUrl = photoUrl
        this.idRoute = idRoute
        this.idReceived = idReceived
        this.type = type
    }

}