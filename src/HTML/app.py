from flask import Flask, render_template
import mysql.connector

app = Flask(__name__, template_folder="templates")  # ✅ Explicitly setting templates folder

# Database Configuration
db_config = {
    "host": "localhost",
    "user": "root",
    "password": "1234",  # Change this if needed
    "database": "tbcode"
}

def get_errors_and_solutions():
    """Fetch errors and solutions from the database"""
    connection = mysql.connector.connect(**db_config)
    cursor = connection.cursor(dictionary=True)

    query = """
    SELECT e.Header, s.CodeSolution, s.SolutionScore 
    FROM Solutions s 
    JOIN Errors e ON s.ErrorID = e.ErrorID 
    ORDER BY s.SolutionScore DESC;
    """
    
    cursor.execute(query)
    results = cursor.fetchall()
    cursor.close()
    connection.close()

    return results

@app.route("/")
def home():
    errors = get_errors_and_solutions()
    return render_template("forum.html", errors=errors)  # ✅ This should work now

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=True)  # ✅ Runs locally on http://127.0.0.1:5000
