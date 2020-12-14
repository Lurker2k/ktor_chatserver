package chat.dataclass

data class UserInfo (val info : String, val infoID : Int)
{
    companion object{
        const val info_column = "userinfo"
        const val infoID_column = "userinfoID"
    }
}
