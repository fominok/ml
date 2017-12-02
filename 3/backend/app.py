import os
from flask import Flask, request, redirect, url_for, jsonify, flash
from werkzeug.utils import secure_filename
from flask_cors import CORS
import ml

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = '/tmp/upload'
CORS(app)

results = None

@app.route("/")
def hello():
    return "Hello world"

@app.route('/loaded')
def loaded():
    return jsonify({"results": results})

@app.route('/upload', methods=['GET', 'POST'])
def upload_file():
    if request.method == 'POST':
        # check if the post request has the file part
        if 'file' not in request.files:
            flash('No file part')
            return redirect(request.url)
        file = request.files['file']
        # if user does not select file, browser also
        # submit a empty part without filename
        if file.filename == '':
            flash('No selected file')
            return redirect(request.url)
        class_field = request.form['class_field']
        filename = secure_filename(file.filename)
        path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(path)
        global results
        results = ml.main(path, class_field)
        return jsonify({"results": results})
