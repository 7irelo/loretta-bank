import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export default function HomePage() {
  redirect(cookies().get("lb_user")?.value ? "/dashboard" : "/login");
}
