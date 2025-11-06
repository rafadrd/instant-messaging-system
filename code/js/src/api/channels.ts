import { Channel, ChannelInput } from "../types";
import { apiRequest } from "../utils/api";

export const createChannel = async (input: ChannelInput): Promise<Channel> => {
  return apiRequest<Channel>("/api/channels", {
    method: "POST",
    body: JSON.stringify(input),
  });
};

export const fetchChannelDetails = async (
  channelId: number,
): Promise<Channel> => {
  return apiRequest<Channel>(`/api/channels/${channelId}`, {
    method: "GET",
  });
};

export const searchChannels = async (
  query: string = "",
): Promise<Channel[]> => {
  const url = query ? `/api/channels?query=${query}` : "/api/channels";
  return apiRequest<Channel[]>(url, {
    method: "GET",
  });
};

export const joinChannel = async (channelId: number): Promise<void> => {
  return apiRequest<void>(`/api/channels/${channelId}/join`, {
    method: "POST",
  });
};

export const joinChannelByToken = async (token: string): Promise<void> => {
  return apiRequest<void>(`/api/channels/join-by-token`, {
    method: "POST",
    body: JSON.stringify({ token }),
  });
};

export const leaveChannel = async (channelId: number): Promise<string> => {
  return apiRequest<string>(`/api/channels/${channelId}/leave`, {
    method: "POST",
  });
};

export const fetchMemberAccessType = async (
  userId: number,
  channelId: number,
): Promise<string> => {
  return apiRequest<string>(`/api/channels/${channelId}/members/${userId}`, {
    method: "GET",
  });
};
