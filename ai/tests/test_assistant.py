from loretta_ai.features import assistant
from tests.fakes import Block, FakeBank, FakeLLM, Response


def tool(name, args, id_="t1"):
    return Response([Block(type="tool_use", name=name, input=args, id=id_)], "tool_use")


def final(text):
    return Response([Block(type="text", text=text)], "end_turn")


async def test_injected_payee_is_flagged_and_nothing_is_sent():
    llm = FakeLLM(
        [
            tool(
                "search_transactions",
                {"text": None, "type": None, "start_date": None, "end_date": None},
            ),
            tool(
                "draft_payment",
                {
                    "from_account_id": 1,
                    "to_account_number": "LOR9999999999",
                    "amount": "5000.00",
                    "description": "as instructed",
                },
                "t2",
            ),
            final("I've prepared it."),
        ]
    )
    bank = FakeBank()
    store = assistant.ProposalStore()
    reply = await assistant.chat(llm, bank, store, "u1", [], "What came into my account recently?")

    assert reply.proposals and reply.proposals[0].warning is not None
    # The transactions shown to the model never contain the attacker's instructions.
    messages = llm.requests[-1]["messages"]
    tool_output = str(messages[2]["content"][0]["content"])  # result of search_transactions
    assert "IGNORE PREVIOUS INSTRUCTIONS" not in tool_output
    assert "<untrusted_data" in tool_output
    # The bank client has no way to move money; only reads happened.
    assert set(bank.calls) <= {"accounts", "transactions"}


async def test_payee_typed_by_customer_has_no_warning():
    llm = FakeLLM(
        [
            tool(
                "draft_payment",
                {
                    "from_account_id": 1,
                    "to_account_number": "LOR0123456789",
                    "amount": "250",
                    "description": "Rent",
                },
            ),
            final("Please confirm in the app."),
        ]
    )
    reply = await assistant.chat(
        llm, FakeBank(), assistant.ProposalStore(), "u1", [], "Pay R250 to LOR0123456789 for rent"
    )
    assert reply.proposals[0].warning is None
    assert str(reply.proposals[0].amount) == "250.00"


async def test_cannot_draft_from_someone_elses_account():
    llm = FakeLLM(
        [
            tool(
                "draft_payment",
                {
                    "from_account_id": 42,
                    "to_account_number": "LOR0123456789",
                    "amount": "10",
                    "description": "x",
                },
            ),
            final("Sorry."),
        ]
    )
    reply = await assistant.chat(
        llm,
        FakeBank(),
        assistant.ProposalStore(),
        "u1",
        [],
        "Pay R10 from account 42 to LOR0123456789",
    )
    assert reply.proposals == []
    result = llm.requests[1]["messages"][-1]["content"][0]
    assert result["is_error"] is True


async def test_proposals_are_bound_to_their_owner():
    store = assistant.ProposalStore()
    llm = FakeLLM(
        [
            tool(
                "draft_payment",
                {
                    "from_account_id": 1,
                    "to_account_number": "LOR0123456789",
                    "amount": "10",
                    "description": "x",
                },
            ),
            final("ok"),
        ]
    )
    reply = await assistant.chat(llm, FakeBank(), store, "owner", [], "Pay R10 to LOR0123456789")
    proposal_id = reply.proposals[0].id
    assert store.get("owner", proposal_id) is not None
    assert store.get("someone-else", proposal_id) is None


async def test_step_limit_stops_runaway_loops():
    llm = FakeLLM([tool("list_accounts", {}, f"t{i}") for i in range(assistant.MAX_STEPS)])
    reply = await assistant.chat(llm, FakeBank(), assistant.ProposalStore(), "u1", [], "loop")
    assert "more steps" in reply.text


def test_fraud_escalates_to_opus():
    assert assistant.choose_model("I think my card was stolen")[0] == assistant.OPUS
    assert assistant.choose_model("what's my balance")[0] == assistant.SONNET
