import { LoginInput, RegisterInput, User } from "../types";
import { apiRequest } from "../utils/api";

export const registerUser = async (input: RegisterInput): Promise<User> => {
  return apiRequest<User>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(input),
  });
};

export const loginUser = async (input: LoginInput): Promise<User> => {
  return apiRequest<User>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(input),
  });
};

export const logoutUser = async (): Promise<void> => {
  await apiRequest<void>("/api/auth/logout", {
    method: "POST",
  });
};
