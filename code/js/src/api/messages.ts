import { Message } from "../types";
import { apiRequest } from "../utils/api";

export const fetchMessages = async (channelId: number): Promise<Message[]> => {
  return apiRequest<Message[]>(`/api/channels/${channelId}/messages`, {
    method: "GET",
  });
};

export const postNewMessage = async (
  channelId: number,
  content: string,
): Promise<Message> => {
  return apiRequest<Message>(`/api/channels/${channelId}/messages`, {
    method: "POST",
    body: JSON.stringify({ content }),
  });
};
