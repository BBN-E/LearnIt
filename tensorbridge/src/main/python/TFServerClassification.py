from flask import Flask, request, g, Response, jsonify
import sys
import json
import os

def initDecoder(decoderClass,paramsFile):
    if len(decoderClass) == 0:
        print "Empty decoder class not permitted"
        sys.exit(1)
    parts = decoderClass.split('.')
    module = ".".join(parts[:-1])
    print "Attempting to import module: %s" % module
    m = __import__(module)
    print dir(m)
    for part in parts[1:]:
        print "Processing part %s" % part
        m = getattr(m, part)
    return m(paramsFile)

app = Flask(__name__)
app.config.from_envvar('TF_SERVER_PARAMS')

decoder = initDecoder(app.config['DECODER_CLASS'], app.config['PARAMS'])
print "Decoder object initialized"

@app.route("/ready", methods=['GET'])
def ready():
    return "true"

@app.route("/decode", methods=['POST'])
def decode():
    json_ret = decoder.decode(request.get_json())
    decoded ={'label': json_ret['label'], 'confidence': json_ret['confidence']}

    return jsonify(decoded)

@app.route("/shutdown", methods=['POST'])
def shutdown():
    decoder.shutdown()
    # see http://flask.pocoo.org/snippets/67/
    shutdown_func = request.environ.get("werkzeug.server.shutdown")
    if shutdown_func is None:
        raise RuntimeError("Could not get shutdown function")
    shutdown_func()
    return "bye"

port = os.environ['TF_SERVER_PORT']
print "Running Tensorflow server on port %s" % port
app.run(port=port)

