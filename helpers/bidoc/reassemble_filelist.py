import os


def main():
    source_list = ""
    new_arabic_folder = ""
    dst_list = ""
    output_buffer = list()
    with open(source_list) as fp:
        for i in fp:
            i = i.strip()
            fields = i.split(" ")
            entries = {i.split(":")[0]: i.split(":")[1] for i in fields}
            if os.path.isfile(os.path.join(new_arabic_folder, "{}.xml".format(entries['docid']))):
                entries['arabic'] = os.path.join(new_arabic_folder, "{}.xml".format(entries['docid']))
            else:
                print(entries['docid'])
            output_buffer.append(" ".join(":".join((k, v)) for k, v in entries.items()))
    with open(dst_list, 'w') as wfp:
        for i in output_buffer:
            wfp.write("{}\n".format(i))


if __name__ == "__main__":
    main()
