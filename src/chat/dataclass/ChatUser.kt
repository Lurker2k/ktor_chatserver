package chat.dataclass

data class ChatUser(
    var socketID : String = "",
    var loginTime : String = "",
    var name : String? = null,
    var lastLogin : String = "",
    var neueNachrichten : Int = 0,
    var lastMessage : String = ""
)