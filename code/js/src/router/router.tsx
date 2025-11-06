import * as React from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import LoginPage from "../pages/auth/LoginPage";
import RegisterPage from "../pages/auth/RegisterPage";
import Layout from "../components/Layout";
import AuthRequire from "../components/AuthRequire";
import JoinedChannelsPage from "../pages/channels/JoinedChannelsPage";
import ChannelDetailPage from "../pages/channels/ChannelDetailPage";
import SearchChannelsPage from "../pages/channels/SearchChannelsPage";
import CreateChannelPage from "../pages/channels/CreateChannelPage";
import GenerateInvitationPage from "../pages/invitations/GenerateInvitationPage";
import ManageInvitationsPage from "../pages/invitations/ManageInvitationsPage";
import AboutPage from "../pages/about/AboutPage";
import NotFoundPage from "../pages/NotFoundPage";
import RootRedirect from "../components/RootRedirect";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,
    children: [
      { index: true, element: <RootRedirect /> },
      { path: "/login", element: <LoginPage /> },
      { path: "/register", element: <RegisterPage /> },
      {
        path: "/channels",
        element: (
          <AuthRequire>
            <JoinedChannelsPage />
          </AuthRequire>
        ),
      },
      {
        path: "/channels/:id",
        element: (
          <AuthRequire>
            <ChannelDetailPage />
          </AuthRequire>
        ),
      },
      {
        path: "/channels/:id/manage-invitations",
        element: (
          <AuthRequire>
            <ManageInvitationsPage />
          </AuthRequire>
        ),
      },
      {
        path: "/search-channels",
        element: (
          <AuthRequire>
            <SearchChannelsPage />
          </AuthRequire>
        ),
      },
      {
        path: "/create-channel",
        element: (
          <AuthRequire>
            <CreateChannelPage />
          </AuthRequire>
        ),
      },
      {
        path: "/generate-invitation",
        element: (
          <AuthRequire>
            <GenerateInvitationPage />
          </AuthRequire>
        ),
      },
      {
        path: "/about",
        element: (
          <AuthRequire>
            <AboutPage />
          </AuthRequire>
        ),
      },
      {
        path: "*",
        element: (
          <AuthRequire>
            <NotFoundPage />
          </AuthRequire>
        ),
      },
    ],
  },
]);

export default router;
