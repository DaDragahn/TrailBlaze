package dam.a42363.trailblaze.models

class RouteInfo {
    var name: String? = null
    var localidade: String? = null
    var author: String? = null
    var uid: String? = null

    constructor() {}
    constructor(name: String?, localidade: String?, author: String?, id: String?) {
        this.name = name
        this.localidade = localidade
        this.author = author
        this.uid = id
    }
}