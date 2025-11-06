import * as React from "react";
import { Link, NavLink } from "react-router-dom";
import LogoutButton from "./LogoutButton";
import "./CSS/NavBar.css";
import useAuth from "../hooks/auth/useAuth";

const Navbar = () => {
  const { user, loading } = useAuth();

  if (loading) return null;

  return (
    <div className="navbar">
      <Link to="/" className="navbar-link">
        IM System
      </Link>

      <div className="navbar-items">
        {user ? (
          <>
            <NavLink
              to="/channels"
              className={({ isActive }) =>
                `navbar-item ${isActive ? "navbar-item-active" : ""}`
              }
            >
              My Channels
            </NavLink>

            <NavLink
              to="/create-channel"
              className={({ isActive }) =>
                `navbar-item ${isActive ? "navbar-item-active" : ""}`
              }
            >
              Create Channel
            </NavLink>

            <NavLink
              to="/search-channels"
              className={({ isActive }) =>
                `navbar-item ${isActive ? "navbar-item-active" : ""}`
              }
            >
              Search Channels
            </NavLink>

            <NavLink
              to="/generate-invitation"
              className={({ isActive }) =>
                `navbar-item ${isActive ? "navbar-item-active" : ""}`
              }
            >
              Invitations
            </NavLink>

            <NavLink
              to="/about"
              className={({ isActive }) =>
                `navbar-item ${isActive ? "navbar-item-active" : ""}`
              }
            >
              About
            </NavLink>

            <span className="hello-user">Hello, {user.username}</span>

            <LogoutButton className="navbar-item logout-button">
              Logout <i className="fas fa-arrow-right"></i>
            </LogoutButton>
          </>
        ) : (
          <>
            <NavLink
              to="/login"
              className={({ isActive }) =>
                `navbar-item ${isActive ? "navbar-item-active" : ""}`
              }
            >
              Login
            </NavLink>
            <NavLink
              to="/register"
              className={({ isActive }) =>
                `navbar-item ${isActive ? "navbar-item-active" : ""}`
              }
            >
              Register
            </NavLink>
          </>
        )}
      </div>
    </div>
  );
};

export default Navbar;
