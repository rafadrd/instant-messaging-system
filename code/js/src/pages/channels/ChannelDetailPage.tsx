import * as React from "react";
import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import LoadingSpinner from "../../components/LoadingSpinner";
import { useParams } from "react-router-dom";
import "../CSS/ChannelDetailPage.css";
import useFetchChannel from "../../hooks/channels/useFetchChannel";
import useFetchAccessType from "../../hooks/channels/useFetchAccessType";
import useFetchMessages from "../../hooks/messages/useFetchMessages";
import useAuth from "../../hooks/auth/useAuth";
import usePostMessage from "../../hooks/messages/usePostMessage";
import useLeaveChannel from "../../hooks/channels/useLeaveChannel";
import { Message } from "../../types";
import useMessageSSE from "../../hooks/messages/useMessageSSE";
import Header from "../../components/Header";
import MessageComponent from "../../components/MessageComponent";
import MessageInput from "../../components/MessageInput";
import LeaveChannelModal from "../../components/LeaveChannelModal";

const ChannelDetailPage = () => {
  const { user } = useAuth();
  const { id } = useParams<{ id: string }>();
  const channelId = id ? parseInt(id, 10) : null;

  const {
    data: channel,
    isLoading: isChannelLoading,
    error: channelError,
  } = useFetchChannel(channelId);

  const {
    data: accessType,
    isLoading: isAccessTypeLoading,
    error: accessTypeError,
  } = useFetchAccessType(channelId);

  const {
    messages: initialMessages,
    loading: isMessagesLoading,
    error: messagesError,
  } = useFetchMessages(channelId);

  const [messages, setMessages] = useState<Message[]>([]);

  useEffect(() => {
    if (initialMessages) {
      setMessages(initialMessages);
    }
  }, [initialMessages]);

  const {
    handlePostMessage: postNewMessage,
    loading: isPosting,
    error: postError,
  } = usePostMessage(channelId);

  const {
    leaveChannel,
    loading: isLeaving,
    error: leaveError,
  } = useLeaveChannel(channelId);

  const [newMessage, setNewMessage] = useState<string>("");
  const [showModal, setShowModal] = useState<boolean>(false);
  const [generalError, setGeneralError] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, []);

  const handleNewMessage = useCallback(
    (message: Message) => {
      setMessages((prevMessages: Message[]) => {
        if (prevMessages.some((m: Message) => m.id === message.id)) {
          return prevMessages;
        }
        return [...prevMessages, message];
      });
      scrollToBottom();
    },
    [scrollToBottom],
  );

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  useMessageSSE({ channelId: channelId!, onNewMessage: handleNewMessage });

  const handlePostMessageSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await postNewMessage(newMessage);
    setNewMessage("");
  };

  const handleLeaveChannel = async () => await leaveChannel();

  if (channelId === null) {
    return <div className="error-message">Invalid channel ID.</div>;
  }

  if (!user) {
    return (
      <div className="error-message">
        You must be logged in to view this page.
      </div>
    );
  }

  if (isChannelLoading || isAccessTypeLoading || isMessagesLoading) {
    return <LoadingSpinner />;
  }

  if (channelError || accessTypeError || messagesError) {
    return (
      <div className="error-container">
        {channelError && <p>{channelError.message}</p>}
        {accessTypeError && <p>{accessTypeError.message}</p>}
        {messagesError && <p>{messagesError}</p>}
      </div>
    );
  }

  if (!channel) {
    return <div className="no-channel">No channel found.</div>;
  }

  return (
    <div className="chat-container">
      <Header
        channel={channel}
        channelId={channelId}
        onLeave={() => setShowModal(true)}
      />

      <div className="chat-messages">
        {isMessagesLoading ? (
          <LoadingSpinner />
        ) : messages.length === 0 ? (
          <div className="no-messages">
            No messages yet. Start the conversation!
          </div>
        ) : (
          messages.map((message) => (
            <MessageComponent
              key={message.id}
              message={message}
              currentUser={user}
            />
          ))
        )}
        <div ref={messagesEndRef} />
        {messagesError && <div className="error-message">{messagesError}</div>}
      </div>

      {accessType && accessType !== "READ_ONLY" && (
        <MessageInput
          newMessage={newMessage}
          setNewMessage={setNewMessage}
          handleSubmit={handlePostMessageSubmit}
          isPosting={isPosting}
          postError={postError}
          generalError={generalError}
        />
      )}

      <LeaveChannelModal
        channel={channel}
        show={showModal}
        onClose={() => setShowModal(false)}
        onConfirm={handleLeaveChannel}
        isLeaving={isLeaving}
        leaveError={leaveError}
      />
    </div>
  );
};

export default ChannelDetailPage;
