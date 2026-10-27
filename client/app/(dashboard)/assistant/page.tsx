"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRef, useState, type FormEvent } from "react";
import { AlertTriangle, Mic, Send, Sparkles, Square } from "lucide-react";

import { Amount } from "@/components/form";
import { SectionHeader } from "@/components/section-header";
import { StepUpDialog } from "@/components/step-up-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { aiApi, errorMessage, isStepUpRequired, newIdempotencyKey, transactionsApi } from "@/lib/api";
import { cn } from "@/lib/utils";
import type { PaymentProposal } from "@/types/api";

interface Message {
  role: "user" | "assistant";
  text: string;
  proposals?: PaymentProposal[];
  handoff?: string | null;
}

function ProposalCard({ proposal }: { proposal: PaymentProposal }) {
  const queryClient = useQueryClient();
  const [state, setState] = useState<"idle" | "sending" | "done" | "error">("idle");
  const [message, setMessage] = useState<string | null>(null);
  const [stepUp, setStepUp] = useState(false);
  const key = useRef(newIdempotencyKey());

  const confirm = async () => {
    setState("sending");
    try {
      // Re-read the proposal from the server so what executes is what the server stored,
      // never text the model wrote into the chat.
      const stored = await aiApi.proposal(proposal.id);
      const transfer = await transactionsApi.transfer(
        {
          sourceAccountId: stored.from_account_id,
          targetAccountNumber: stored.to_account_number,
          amount: Number(stored.amount),
          description: stored.description || undefined
        },
        key.current
      );
      setState("done");
      setMessage(transfer.status === "PENDING" ? "Payment is processing." : `Paid. Reference ${transfer.reference}.`);
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
    } catch (err) {
      if (isStepUpRequired(err)) {
        setStepUp(true);
        setState("idle");
        return;
      }
      setState("error");
      setMessage(errorMessage(err, "The payment was not made."));
    }
  };

  return (
    <div className="mt-3 rounded-xl border border-line bg-white p-4 text-sm">
      <p className="font-semibold text-ink">Payment for you to review</p>
      <dl className="mt-2 space-y-1">
        <div className="flex justify-between"><dt className="text-slate">From</dt><dd>{proposal.from_account_number}</dd></div>
        <div className="flex justify-between"><dt className="text-slate">To</dt><dd className="font-mono">{proposal.to_account_number}</dd></div>
        <div className="flex justify-between"><dt className="text-slate">Amount</dt><dd><Amount value={Number(proposal.amount)} /></dd></div>
      </dl>
      {proposal.warning ? (
        <p className="mt-3 flex gap-2 rounded-lg bg-danger/10 p-2 text-danger"><AlertTriangle className="h-4 w-4 shrink-0" />{proposal.warning}</p>
      ) : null}
      {message ? <p className={cn("mt-3", state === "error" ? "text-danger" : "text-success")}>{message}</p> : null}
      {state !== "done" ? (
        <div className="mt-3 flex gap-2">
          <Button size="sm" onClick={confirm} disabled={state === "sending"}>{state === "sending" ? "Paying…" : "Confirm and pay"}</Button>
        </div>
      ) : null}
      <StepUpDialog open={stepUp} onCancel={() => setStepUp(false)} onConfirmed={() => { setStepUp(false); void confirm(); }} />
    </div>
  );
}

export default function AssistantPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [pending, setPending] = useState(false);
  const [recording, setRecording] = useState(false);
  const recorder = useRef<MediaRecorder | null>(null);
  const chunks = useRef<Blob[]>([]);

  const history = () => messages.slice(-10).map((m) => ({ role: m.role, text: m.text.slice(0, 2000) }));

  const send = async (event?: FormEvent) => {
    event?.preventDefault();
    const text = input.trim();
    if (!text || pending) {
      return;
    }
    setInput("");
    setMessages((prev) => [...prev, { role: "user", text }]);
    setPending(true);
    try {
      const reply = await aiApi.chat(text, history());
      setMessages((prev) => [...prev, { role: "assistant", text: reply.text, proposals: reply.proposals, handoff: reply.handoff }]);
    } catch (err) {
      setMessages((prev) => [...prev, { role: "assistant", text: errorMessage(err, "I'm unavailable right now. Everything else in the app still works.") }]);
    } finally {
      setPending(false);
    }
  };

  const toggleVoice = async () => {
    if (recording) {
      recorder.current?.stop();
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const rec = new MediaRecorder(stream, { mimeType: "audio/webm" });
      chunks.current = [];
      rec.ondataavailable = (e) => chunks.current.push(e.data);
      rec.onstop = async () => {
        stream.getTracks().forEach((t) => t.stop());
        setRecording(false);
        setPending(true);
        try {
          const result = await aiApi.voice(new Blob(chunks.current, { type: "audio/webm" }));
          setMessages((prev) => [
            ...prev,
            { role: "user", text: result.transcript },
            { role: "assistant", text: result.reply.text, proposals: result.reply.proposals, handoff: result.reply.handoff }
          ]);
          if (result.audio_base64) {
            void new Audio(`data:audio/mpeg;base64,${result.audio_base64}`).play();
          }
        } catch (err) {
          setMessages((prev) => [...prev, { role: "assistant", text: errorMessage(err, "I couldn't hear that. Try again or type your question.") }]);
        } finally {
          setPending(false);
        }
      };
      recorder.current = rec;
      rec.start();
      setRecording(true);
      setTimeout(() => rec.state === "recording" && rec.stop(), 30_000);
    } catch {
      setMessages((prev) => [...prev, { role: "assistant", text: "Microphone access was blocked." }]);
    }
  };

  return (
    <div className="mx-auto flex h-[calc(100vh-10rem)] max-w-3xl flex-col">
      <SectionHeader title="Assistant" description="Ask about your money. I can prepare payments; you always confirm them." />
      <Card className="flex min-h-0 flex-1 flex-col">
        <CardContent className="flex-1 space-y-4 overflow-y-auto" aria-live="polite">
          {messages.length === 0 ? (
            <div className="space-y-2 text-sm text-slate">
              <p className="flex items-center gap-2 font-medium text-ink"><Sparkles className="h-4 w-4 text-primary" /> Try asking</p>
              {["What's my balance?", "How much did I spend on groceries this month?", "What are the fees for ATM withdrawals?"].map((s) => (
                <button key={s} className="block rounded-lg border border-line px-3 py-2 text-left hover:border-primary/40" onClick={() => setInput(s)}>{s}</button>
              ))}
            </div>
          ) : null}
          {messages.map((m, i) => (
            <div key={i} className={cn("max-w-[85%]", m.role === "user" ? "ml-auto" : "")}>
              {/* Rendered as plain text: model output never becomes HTML or links. */}
              <p className={cn("whitespace-pre-wrap rounded-2xl px-4 py-2 text-sm",
                m.role === "user" ? "bg-primary text-white" : "bg-surface text-ink")}>{m.text}</p>
              {m.handoff ? <p className="mt-2 text-xs text-slate">A support agent will follow up on this conversation.</p> : null}
              {m.proposals?.map((p) => <ProposalCard key={p.id} proposal={p} />)}
            </div>
          ))}
          {pending ? <p className="text-sm text-slate">Thinking…</p> : null}
        </CardContent>
        <form onSubmit={send} className="flex gap-2 border-t border-line p-4">
          <Button type="button" variant={recording ? "danger" : "secondary"} onClick={toggleVoice}
            aria-label={recording ? "Stop recording" : "Speak"} disabled={pending && !recording}>
            {recording ? <Square className="h-4 w-4" /> : <Mic className="h-4 w-4" />}
          </Button>
          <Input value={input} onChange={(e) => setInput(e.target.value)} maxLength={2000} placeholder="Ask anything about your accounts" aria-label="Message" />
          <Button type="submit" disabled={pending || !input.trim()} aria-label="Send"><Send className="h-4 w-4" /></Button>
        </form>
      </Card>
    </div>
  );
}
