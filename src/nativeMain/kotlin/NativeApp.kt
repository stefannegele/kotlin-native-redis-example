import hiredis.*
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKStringFromUtf8

fun main() {
    val context = requireNotNull(redisConnect("localhost", 6379)) { "Can't allocate redis context" }
        .apply {
            if (pointed.err > 0) {
                throw RuntimeException("Can not connect to redis: ${pointed.errstr.toKStringFromUtf8()}")
            }
        }

    val reply = redisCommand(context, "XREAD STREAMS events 0")

    reply?.reinterpret<redisReply>()?.pointed?.convert()
        ?.let { println(it) }

    freeReplyObject(reply)
}

private fun redisReply.convert(): RedisReply = when (this.type) {
    REDIS_REPLY_STRING -> this.convertString()
    REDIS_REPLY_ARRAY -> this.convertArray()
    else -> throw IllegalStateException("Illegal redis reply type: ${this.type}")
}

private fun redisReply.convertString() = RedisReply(RedisReply.Type.ReplyString, string = str?.toKStringFromUtf8())
private fun redisReply.convertArray() = RedisReply(RedisReply.Type.ReplyArray, elements = convertElements())

private fun redisReply.convertElements(): List<RedisReply> = List(elements.toInt()) {
    element?.get(it)?.pointed?.convert() ?: throw IllegalStateException("")
}

data class RedisReply(
    val type: Type,
    val elements: List<RedisReply>? = null,
    val string: String? = null
) {
    enum class Type {
        ReplyString,
        ReplyArray
    }
}
