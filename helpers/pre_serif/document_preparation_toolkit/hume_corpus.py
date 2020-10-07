import io
import os

bad_characters = {(237, 160, 189,), (237, 180, 146,), (237, 160, 190,), (237, 182, 129,), (237, 160, 188,),
                  (237, 183, 183,),
                  (237, 160, 181,), (237, 177, 159,), (226, 128, 162,), (226, 128, 147,), (226, 128, 166,),
                  (226, 128, 156,),
                  (240, 159, 143,),
                  (226, 142, 167,),
                  (226, 137, 160,),
                  (226, 128, 145,),
                  (238, 129, 184,),
                  (240, 159, 144,),
                  (226, 136, 151,),
                  (226, 150, 186,),
                  (226, 134, 145,),
                  (226, 153, 166,),
                  (239, 131, 153,),
                  (226, 128, 148,),
                  (226, 142, 160,),
                  (239, 129, 134,),
                  (226, 142, 169,),
                  (226, 128, 149,),
                  (226, 128, 138,),
                  (226, 136, 188,),
                  (226, 139, 129,),
                  (226, 128, 146,),
                  (227, 128, 136,),
                  (226, 128, 153,),
                  (227, 128, 137,),
                  (226, 130, 172,),
                  (226, 128, 179,),
                  (226, 136, 134,),
                  (240, 159, 145,),
                  (240, 159, 148,),
                  (226, 136, 146,),
                  (226, 147, 130,),
                  (226, 128, 178,),
                  (239, 129, 177,),
                  (226, 150, 170,),
                  (239, 130, 183,),
                  (226, 128, 160,),
                  (226, 158, 164,),
                  (226, 136, 171,),
                  (226, 128, 144,),
                  (226, 134, 147,),
                  (226, 150, 182,),
                  (226, 128, 151,),
                  (239, 128, 178,),
                  (226, 142, 155,),
                  (226, 138, 130,),
                  (239, 128, 173,),
                  (240, 159, 152,),
                  (226, 136, 136,),
                  (226, 142, 170,),
                  (239, 128, 162,),
                  (226, 134, 151,),
                  (226, 152, 170,),
                  (226, 137, 164,),
                  (226, 150, 188,),
                  (226, 151, 143,),
                  (239, 129, 181,),
                  (226, 137, 133,),
                  (226, 136, 154,),
                  (226, 136, 130,),
                  (226, 140, 169,),
                  (226, 129, 142,),
                  (226, 151, 188,),
                  (226, 137, 161,),
                  (226, 156, 147,),
                  (238, 128, 157,),
                  (226, 135, 145,),
                  (226, 142, 158,),
                  (226, 128, 159,),
                  (226, 128, 158,),
                  (226, 140, 170,),
                  (239, 131, 154,),
                  (226, 128, 150,),
                  (226, 139, 128,),
                  (226, 137, 136,),
                  (226, 128, 157,),
                  (226, 142, 157,),
                  (226, 128, 152,),
                  (226, 150, 160,),
                  (238, 128, 158,),
                  (226, 157, 150,),
                  (239, 129, 176,),
                  (226, 142, 168,),
                  (226, 139, 133,),
                  (240, 159, 133,),
                  (226, 156, 148,),
                  (239, 187, 191,),
                  (226, 151, 166,),
                  (226, 150, 178,),
                  (226, 128, 175,),
                  (226, 128, 137,),
                  (226, 136, 145,),
                  (240, 159, 146,),
                  (226, 132, 162,),
                  (226, 137, 165,),
                  (226, 132, 131,),
                  (226, 145, 162,),
                  (226, 132, 142,),
                  (226, 145, 161,),
                  (239, 172, 129,),
                  (239, 172, 128,),
                  (239, 172, 131,),
                  (226, 145, 160,),
                  (239, 172, 130,)
                  }
bad_characters_four = {
    (240, 157, 145, 133,),
    (240, 157, 145, 146,),
    (240, 157, 145, 163,),
    (240, 157, 145, 147,),
    (240, 157, 144, 190,),
    (240, 157, 145, 139,),
    (240, 157, 145, 150,),
    (240, 157, 145, 159,),
    (240, 157, 156, 130,),
    (240, 157, 145, 156,),
    (240, 157, 145, 165,),
    (240, 157, 155, 188,),
    (240, 157, 145, 142,),
    (240, 157, 145, 152,),
    (240, 157, 145, 161,),
    (240, 157, 156, 128,),
    (240, 157, 145, 155,),
    (240, 157, 156, 147,),
    (240, 157, 144, 185,),
    (240, 157, 145, 145,),
    (240, 157, 145, 131,),
    (240, 157, 145, 157,),
    (240, 157, 156, 142,),
    (240, 157, 145, 135,),
    (240, 157, 145, 160,),
    (240, 157, 145, 167,)
}

def is_bad_character(c):
    if c == "<" or c == ">":
        return True
    bytes = c.encode(encoding='utf8', errors='strict')
    if len(bytes) < 3:
        return False

    key = (bytes[0], bytes[1], bytes[2],)
    if key in bad_characters:
        # print "Skipping character in " + filename
        return True
    if len(bytes) == 4:
        key = (bytes[0], bytes[1], bytes[2], bytes[3])
        if key in bad_characters_four:
            return True
    # if True:
    #     if len(bytes) == 3:
    #         print("{} {} {}".format(bytes[0],bytes[1],bytes[2]))
    #     if len(bytes) == 4:
    #         print("{} {} {} {}".format(bytes[0],bytes[1],bytes[2],bytes[3]))
    return False


def next_good_character_is_newline(utext, pos):
    for i in range(10):
        if utext[pos + i] == "\n":
            return True
        if utext[pos + i].isalpha():
            return False
    return False


def is_good_break_point(char_pos, len_utext, c, utext):
    if char_pos > len_utext - 10000:
        return False
    if c != "\n":
        return False

    return utext[char_pos] == "\n" and next_good_character_is_newline(utext, char_pos + 1)


def break_text_into_array_of_text(text, approximiate_length_per_piece=30000):
    # escaped_text_1 = remove_weird_characters(text)
    escaped_text_1 = text

    escaped_text_2 = ""
    for c in escaped_text_1:
        if is_bad_character(c) is True:
            escaped_text_2 += " "
        else:
            escaped_text_2 += c

    output_text_buf = list()
    current_escaped_text = ""

    for c in escaped_text_2:
        if len(current_escaped_text) > approximiate_length_per_piece and is_good_break_point(
                len(current_escaped_text) - 1, len(escaped_text_2),
                c, escaped_text_2):
            output_text_buf.append(current_escaped_text)
            current_escaped_text = ""
        current_escaped_text += c
    if len(current_escaped_text) > 0:
        output_text_buf.append(current_escaped_text)

    return output_text_buf


def make_sgm_file(text, original_docId, output_folder, s3_source_uri, source_uri,doc_id_prefix, creation_date, author, booking_arr,
                  sgm_path_list):
    d = creation_date
    output_text_buf = break_text_into_array_of_text(text, 30000)
    char_offset_until_now = 0
    escaped_docId = "{}_{}".format(doc_id_prefix,original_docId)
    os.makedirs(os.path.join(output_folder, 'sgms'), exist_ok=True)
    for idx, j in enumerate(output_text_buf):
        escaped_docId_with_idx = "{}_{}".format(escaped_docId, idx)
        header = "<DOC id=\"{}\">\n<DATETIME>{}</DATETIME>\n<TEXT>\n".format(escaped_docId_with_idx, d)
        tail = "\n</TEXT>\n</DOC>"
        with io.open(os.path.join(output_folder, 'sgms', escaped_docId_with_idx + ".sgm"), 'w', encoding='utf-8') as fp:
            fp.write(header)
            fp.write(j)
            fp.write(tail)
        booking_arr.append("\t".join([escaped_docId_with_idx,
                                      s3_source_uri,
                                      d,
                                      "1.0",
                                      original_docId,
                                      "./sgms/{}".format(escaped_docId_with_idx + ".sgm"),
                                      original_docId,
                                      str(-len(header) + char_offset_until_now),
                                      author,
                                      source_uri
                                      ]))
        char_offset_until_now += len(j)
        sgm_path_list.append(os.path.join(output_folder, 'sgms', escaped_docId_with_idx + ".sgm"))


def make_txt_file(text, original_docId, output_folder):
    os.makedirs(os.path.join(output_folder, 'txts'), exist_ok=True)
    with open(os.path.join(output_folder, 'txts', original_docId + ".txt"), 'w', encoding='utf-8') as fp:
        fp.write(text)
