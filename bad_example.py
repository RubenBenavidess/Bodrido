# bad_example.py
import sqlite3

conn = sqlite3.connect("test.db")
cursor = conn.cursor()

def login(username, password):
    query = (
        "SELECT * FROM users WHERE username = '"
        + username
        + "' AND password = '"
        + password
        + "'"
    )

    print("Ejecutando query:", query)
    cursor.execute(query)

    result = cursor.fetchall()

    if result:
        print("Login exitoso")
    else:
        print("Credenciales inválidas")


# Entrada controlada por el usuario
login("admin' OR '1'='1", "cualquiercosa")
