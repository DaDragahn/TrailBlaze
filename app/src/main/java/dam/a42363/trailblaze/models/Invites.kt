package dam.a42363.trailblaze.models

class Invites {
    var idTrail: String? = null
    var nome: String? = null
    var photoUrl: String? = null
    var idRoute: String? = null

    constructor() {}
    constructor(idTrail: String?, nome: String?, photoUrl: String?, idRoute: String?) {
        this.idTrail = idTrail
        this.nome = nome
        this.photoUrl = photoUrl
        this.idRoute = idRoute
    }

}