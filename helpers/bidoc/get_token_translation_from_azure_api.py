import json
import requests

def chunks(l, n):
    """Yield successive n-sized chunks from l."""
    for i in range(0, len(l), n):
        yield l[i:i + n]


def main(json_path,azure_language,output_json_path):

    with open(json_path) as fp:
        tokens = json.load(fp)

    token_list = tokens

    endpoint = "https://api.cognitive.microsofttranslator.com/translate"
    # endpoint = "https://api.cognitive.microsofttranslator.com/dictionary/lookup"
    key = ""
    headers = {
        "Ocp-Apim-Subscription-Key":key
    }
    ret = dict()
    for tokens_slice in chunks(token_list,100):
        r = requests.post(endpoint,json=[
            {"Text": i}
            for i in tokens_slice],headers=headers,params={"api-version":3.0,"from":azure_language,"to":"en"})
        responese = r.json()
        if "error" in responese:
            print(responese)
        else:
            for i in range(len(tokens_slice)):
                ret[tokens_slice[i]] = responese[i]
    with open(output_json_path,'w') as wfp:
        json.dump(ret,wfp,indent=4,ensure_ascii=False)


if __name__ == "__main__":
    # input_token_file = ""
    # resolved_token_file = ""
    # https://docs.microsoft.com/en-us/azure/cognitive-services/translator/reference/v3-0-translate
    # language list can be found at https://api.cognitive.microsofttranslator.com/languages?api-version=3.0
    # For Arabic, please use "ar"
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--input_token_file",required=True)
    parser.add_argument("--input_azure_language",required=True)
    parser.add_argument("--output_token_with_translation",required=True)
    args = parser.parse_args()
    main(args.input_token_file,args.input_azure_language,args.output_token_with_translation)