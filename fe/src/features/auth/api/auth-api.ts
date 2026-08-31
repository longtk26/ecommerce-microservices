import { apiClient } from "@/lib/api-client";
import type { TLoginRequest, TLoginResponse } from "../types/auth-types";

export async function loginApi(payload: TLoginRequest): Promise<TLoginResponse> {
  return apiClient.post<TLoginResponse>("/api/auth/login", payload);
}

export async function refreshApi(refreshToken: string): Promise<TLoginResponse> {
  return apiClient.post<TLoginResponse>("/api/auth/refresh", { refreshToken });
}
