import axios from "axios";
import type { ApiError } from "../types/api";

export function extractErrorMessage(error: unknown, fallback = "Something went wrong. Please try again."): string {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.message) {
    return error.response.data.message;
  }
  return fallback;
}
