import * as React from "react";
import { FormEvent, useId } from "react";

interface MessageInputProps {
  newMessage: string;
  setNewMessage: React.Dispatch<React.SetStateAction<string>>;
  handleSubmit: (e: FormEvent<HTMLFormElement>) => void;
  isPosting: boolean;
  postError: string | null;
  generalError: string | null;
}

const MessageInput = ({
  newMessage,
  setNewMessage,
  handleSubmit,
  isPosting,
  postError,
  generalError,
}: MessageInputProps) => {
  const errorId = useId();
  const errorMessage = generalError || postError;

  return (
    <form onSubmit={handleSubmit} className="chat-input-container">
      <textarea
        id="newMessage"
        placeholder="Type a message..."
        value={newMessage}
        onChange={(e) => setNewMessage(e.target.value)}
        className="chat-input"
        rows={2}
        disabled={isPosting}
        required
        aria-label="New Message"
        aria-describedby={errorMessage ? errorId : undefined}
        aria-invalid={!!errorMessage}
      ></textarea>
      <button
        type="submit"
        className="send-button"
        disabled={isPosting || !newMessage.trim()}
        aria-label="Send Message"
      >
        {isPosting ? "Sending..." : "Send"}
      </button>
      {errorMessage && (
        <div id={errorId} className="error-message" role="alert">
          {" "}
          {}
          {errorMessage}
        </div>
      )}
    </form>
  );
};

export default MessageInput;
