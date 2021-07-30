package dam.a42363.trailblaze.models

class Grupo {
    var photoUrl: String? = null
    var nome: String? = null
    var groupArray: List<String>? = null

    constructor() {}
    constructor(photoUrl: String?, nome: String?, groupArray: List<String>?) {
        this.photoUrl = photoUrl
        this.nome = nome
        this.groupArray = groupArray
    }
}