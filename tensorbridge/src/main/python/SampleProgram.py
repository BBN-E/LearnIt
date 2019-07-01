# A sample program to be wrapped with TensorFlowDecoderWrapper
class SampleProgram:
    def decode(self, json):
        newName = json['name'][::-1]
        newVal = json['value'] + 100
        return { 'name': newName, 'value' : newVal }

    def shutdown(self):
        pass
