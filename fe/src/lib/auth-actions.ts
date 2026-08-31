"use server";

import { cookies } from "next/headers";

const AUTH_COOKIE_NAME = "shopsaga_auth_token";
const REFRESH_COOKIE_NAME = "shopsaga_refresh_token";

/**
 * Server Action to securely set authentication cookies using Next.js cookies API.
 */
export async function setAuthCookies(accessToken: string, refreshToken?: string): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.set(AUTH_COOKIE_NAME, accessToken, {
    httpOnly: false,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 60 * 24 * 7, // 7 days
  });

  if (refreshToken) {
    cookieStore.set(REFRESH_COOKIE_NAME, refreshToken, {
      httpOnly: false,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 60 * 24 * 30, // 30 days
    });
  }
}

/**
 * Server Action to read the authentication cookie on the server.
 */
export async function getAuthCookie(): Promise<string | undefined> {
  const cookieStore = await cookies();
  return cookieStore.get(AUTH_COOKIE_NAME)?.value;
}

/**
 * Server Action to read the refresh token cookie on the server.
 */
export async function getRefreshTokenCookie(): Promise<string | undefined> {
  const cookieStore = await cookies();
  return cookieStore.get(REFRESH_COOKIE_NAME)?.value;
}

/**
 * Server Action to delete all authentication cookies upon logout.
 */
export async function deleteAuthCookies(): Promise<void> {
  const cookieStore = await cookies();
  cookieStore.delete(AUTH_COOKIE_NAME);
  cookieStore.delete(REFRESH_COOKIE_NAME);
}

// Backward-compatibility aliases
export async function setAuthCookie(token: string): Promise<void> {
  await setAuthCookies(token);
}

export async function deleteAuthCookie(): Promise<void> {
  await deleteAuthCookies();
}
