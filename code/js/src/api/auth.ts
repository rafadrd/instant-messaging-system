import { LoginInput, RegisterInput, User } from "../types";
import { apiRequest } from "../utils/api";

export const registerUser = async (input: RegisterInput): Promise<User> => {
  const user = await apiRequest<User>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(input),
  });
  if (user.token) {
    localStorage.setItem("token", user.token);
  }
  return user;
};

export const loginUser = async (input: LoginInput): Promise<User> => {
  const user = await apiRequest<User>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(input),
  });
  if (user.token) {
    localStorage.setItem("token", user.token);
  }
  return user;
};

export const logoutUser = async (): Promise<void> => {
  try {
    await apiRequest<void>("/api/auth/logout", {
      method: "POST",
    });
  } finally {
    localStorage.removeItem("token");
  }
};
