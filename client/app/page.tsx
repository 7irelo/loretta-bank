import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export default function HomePage() {
  const token = cookies().get("lb_access_token")?.value;

  if (token) {
    redirect("/dashboard");
  }

  redirect("/login");
}

