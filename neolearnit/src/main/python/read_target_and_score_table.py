import json


def read_one_table(target_and_score_tables_path):
    with open(target_and_score_tables_path) as fp:
        main_table = json.load(fp)[1]
    pattern_score_tables = main_table['patternScores'][1]['data'][1]
    pattern_idx_to_pattern = dict()
    for idx, learnit_pattern_en in enumerate(pattern_score_tables['keyList'][1]):
        pattern_idx_to_pattern[idx] = learnit_pattern_en[1]
    score_table_idx_to_score = dict()
    for idx, score_table_en in enumerate(pattern_score_tables['valList'][1]):
        score_table_idx_to_score[idx] = score_table_en[1]
    for en in pattern_score_tables['entries'][1]:
        en = en[1]
        current_pattern = pattern_idx_to_pattern[en['key']]
        current_score = score_table_idx_to_score[en['value']]
        if current_score['precision'] >= 0.5 and current_score[
            'frozen'] is True:  # @hqiu. This is how we define good pattern in TargetAndScoreTables
            print(current_pattern['toIDString'])


if __name__ == "__main__":
    example_score_table = "/home/hqiu/ld100/Hume_pipeline/Hume/resource/learnit_patterns/binary_event_event/Cause-Effect_20190814145325.json"
    read_one_table(example_score_table)
