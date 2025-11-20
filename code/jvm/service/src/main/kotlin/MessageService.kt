package pt.isel

import jakarta.inject.Named

@Named
class MessageService(
    private val trxManager: TransactionManager,
    private val messageEventService: MessageEventService,
) {
    fun createMessage(
        content: String,
        userId: Long,
        channelId: Long,
    ): Either<MessageError, Message> {
        if (content.isBlank()) return failure(MessageError.EmptyMessage)

        val result =
            trxManager.run {
                when (val checkResult = checkUserCanPostMessage(userId, channelId)) {
                    is Failure -> checkResult
                    is Success -> {
                        val (userInfo, channel) = checkResult.value
                        val message = repoMessages.create(content, userInfo, channel)
                        success(message)
                    }
                }
            }

        if (result is Success) {
            messageEventService.broadcastMessage(channelId, NewMessage(result.value))
        }

        return result
    }

    fun getMessagesInChannel(
        userId: Long,
        channelId: Long,
        limit: Int = 50,
        offset: Int = 0,
    ): Either<MessageError, List<Message>> {
        if (limit <= 0) return failure(MessageError.InvalidLimit)
        if (offset < 0) return failure(MessageError.InvalidOffset)

        return trxManager.run {
            val channel =
                repoChannels.findById(channelId) ?: return@run failure(MessageError.ChannelNotFound)

            repoMemberships.findUserInChannel(userId, channelId)
                ?: return@run failure(MessageError.UserNotInChannel)

            val messages = repoMessages.findAllInChannel(channel, limit, offset)

            success(messages)
        }
    }

    private fun Transaction.checkUserCanPostMessage(
        userId: Long,
        channelId: Long,
    ): Either<MessageError, Pair<UserInfo, Channel>> {
        val user = repoUsers.findById(userId) ?: return failure(MessageError.UserNotFound)
        val channel =
            repoChannels.findById(channelId) ?: return failure(MessageError.ChannelNotFound)
        val membership =
            repoMemberships.findUserInChannel(user.id, channel.id)
                ?: return failure(MessageError.UserNotInChannel)

        if (membership.accessType != AccessType.READ_WRITE) {
            return failure(MessageError.UserNotAuthorized)
        }
        val userInfo = UserInfo(user.id, user.username)
        return success(userInfo to channel)
    }
}
