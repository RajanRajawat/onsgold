
#:later

# from dotenv import load_dotenv
# from langchain_google_genai import ChatGoogleGenerativeAI
# from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
# from langchain_core.output_parsers import StrOutputParser
# from langchain_core.runnables.history import RunnableWithMessageHistory
# from langchain_community.chat_message_histories import ChatMessageHistory
# from langchain_huggingface import HuggingFaceEmbeddings
# from langchain_mongodb import MongoDBAtlasVectorSearch
# from langchain_core.runnables import RunnablePassthrough, RunnableLambda
# from langchain_core.output_parsers import StrOutputParser
# from db.database import collection

# load_dotenv()
# llm = ChatGoogleGenerativeAI(model="gemini-2.5-flash", temperature=0)
# embeddings = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")

 
# # vector_store = MongoDBAtlasVectorSearch(
# #     embedding=embeddings,
# #     collection=collection,
# #     index_name="vector_index_wl"
# # )

# # retriever = vector_store.as_retriever(
# #     search_type="similarity",  
# #     search_kwargs={"k": 3} 
# # )

# prompt = ChatPromptTemplate.from_messages([
#     ("system", """
# """),
#     MessagesPlaceholder(variable_name="chat_history"),
#     ("human", "{input}")
# ])

# # def format_docs(docs):
# #     return "\n\n".join(doc.page_content for doc in docs)

# # rag_chain = (
# #     {
# #         "context": (lambda x: x["input"]) | retriever | RunnableLambda(format_docs),
# #         "input":   lambda x: x["input"],
# #         "chat_history": lambda x: x.get("chat_history", [])
# #     }
# #     | prompt
# #     | llm
# #     | StrOutputParser()
# # )

# init_chain = prompt | llm | StrOutputParser()

# store = {} #    store in db
# def get_session_history(session_id: str):
#     if session_id not in store:
#         store[session_id] = ChatMessageHistory()
#     return store[session_id]

# chatbot = RunnableWithMessageHistory(
#     # rag_chain,
#     init_chain,
#     get_session_history,
#     input_messages_key="input",
#     history_messages_key="chat_history"
# )

# def ask_bot(user_input: str, session_id: str = "admin"):
#     config = {"configurable": {"session_id": session_id}}
#     return chatbot.invoke({"input": user_input}, config=config) # type: ignore



# @chatrouter.post("/chat")
# def chat(req: ChatRequest):
#     try:
#         response = ask_bot(req.message, req.session_id)

#         return JSONResponse(
#             status_code=200,
#             content={
#                 "success": True,
#                 "response": response
#             }
#         )

#     except Exception as e:
#         return JSONResponse(
#             status_code=500,
#             content={
#                 "success": False,
#                 "message": str(e)
#             }
#         )
    

# class ChatRequest(BaseModel):
#     message: str = Field(..., min_length=1, max_length=1000)
#     session_id: str = Field(default="admin", min_length=1, max_length=50) 
    
#     @field_validator("message")
#     @classmethod
#     def validate_message(cls, value: str):
#         value = value.strip()

#         if not value:
#             raise ValueError("Message cannot be empty")

#         return value

#     @field_validator("session_id")
#     @classmethod
#     def validate_session_id(cls, value: str):
#         value = value.strip()

#         if not value:
#             return "admin" 

#         return value


