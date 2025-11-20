export interface LoginInput {
  username: string;
  password: string;
}

export interface RegisterInput {
  username: string;
  password: string;
  invitationToken?: string;
}

export interface User {
  id: number;
  username: string;
  token?: string;
}

export interface ChannelInput {
  name: string;
  isPublic: boolean;
}

export interface Channel {
  id: number;
  name: string;
  owner: User;
  isPublic: boolean;
}

export interface InvitationInput {
  channelId: number;
  accessType: "READ_ONLY" | "READ_WRITE";
  expiresAt: string;
}

export interface Invitation {
  id: number;
  token: string;
  createdBy: User;
  channel: Channel;
  accessType: "READ_ONLY" | "READ_WRITE";
  expiresAt: string;
  status: "PENDING" | "ACCEPTED" | "REJECTED";
}

export interface Message {
  id: number;
  content: string;
  user: User;
  channel: Channel;
  createdAt: string;
}