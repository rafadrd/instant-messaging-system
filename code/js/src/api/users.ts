import { Channel, User } from "../types";
import { apiRequest } from "../utils/api";

export const fetchCurrentUser = async (): Promise<User | null> => {
  return apiRequest<User>("/api/users/me", {
    method: "GET",
  });
};

export const fetchUserChannels = async (): Promise<Channel[]> => {
  return apiRequest<Channel[]>(`/api/users/me/channels`, {
    method: "GET",
  });
};
