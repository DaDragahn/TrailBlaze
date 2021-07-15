package dam.a42363.trailblaze.models

class AddFriends {
    var nome: String? = null
    var photoUrl: String? = null

    private constructor() {}
    private constructor(nome: String, photoUrl: String) {
        this.nome = nome
        this.photoUrl = photoUrl
    }
}
