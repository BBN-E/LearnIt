import io
import json
import sys


def main(input_file_list, output_json_path):
    json_path_set = set()
    with open(input_file_list) as fp:
        for i in fp:
            i = i.strip()
            json_path_set.add(i)

    result_list = list()

    for json_path in json_path_set:
        with io.open(json_path, encoding="utf-8") as fp:
            json_en = json.load(fp)
            for i in json_en:
                relation_type = i.get("semantic_class", "NA")
                if relation_type == "NA":
                    continue
                result_list.append(i)

    with io.open(output_json_path, 'w', encoding='utf-8') as fp:
        json.dump(result_list, fp, ensure_ascii=False, sort_keys=True, indent=4)


if __name__ == "__main__":
    input_file_list = sys.argv[1]
    output_json_path = sys.argv[2]
    main(input_file_list, output_json_path)
