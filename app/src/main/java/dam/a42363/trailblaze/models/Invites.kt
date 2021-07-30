package dam.a42363.trailblaze.models

class Invites {
    var idInvite: String? = null
    var nome: String? = null
    var photoUrl: String? = null
    var idRoute: String? = null

    constructor() {}
    constructor(idInvite: String?, nome: String?, photoUrl: String?, idRoute: String?) {
        this.idInvite = idInvite
        this.nome = nome
        this.photoUrl = photoUrl
        this.idRoute = idRoute
    }

}