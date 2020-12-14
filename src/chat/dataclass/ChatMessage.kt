package chat.dataclass


class ChatMessage(
    var nachrichtenID : Int,
    var gesendet_um : String = "",
    var user_gesendet : Boolean = false,
    var nachricht : String = ""
){
  companion object{
      const val nachrichtenID_column = "nachrichtenID"
      const val gesendet_um_column = "gesendet_um"
      const val user_gesendet_column = "user_gesendet"
      const val nachricht_column = "nachricht"


  }
}


