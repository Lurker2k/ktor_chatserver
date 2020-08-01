package chat.dataclass


class ChatMessage(
    var nachrichtenID : Int,
    var gesendet_um : String = "",
    var user_gesendet : Boolean = false,
    var nachricht : String = ""
){
  companion object{
      val nachrichtenID_column = "nachrichtenID"
      val gesendet_um_column = "gesendet_um"
      val user_gesendet_column = "user_gesendet"
      val nachricht_column = "nachricht"


  }
}


