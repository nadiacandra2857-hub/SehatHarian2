import importlib.util, unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('prepare',Path(__file__).parents[1]/'prepare-repdb.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

def item(id='example'):
    return dict(id=id,name_en='Example',difficulty='beginner',body_part='core',category='strength',
                instructions_en=['Step one'],primary_muscles=['abdominals'],images={'flat':{'start':'images/flat/shared-start.webp','peak':'images/flat/shared-peak.webp'}})
def dataset(*items):return dict(schema_version=3,count=len(items),exercises=list(items))

class ParserTest(unittest.TestCase):
    def test_start_peak_and_main(self):
        e=item(); e['images']={'flat':{'main':'images/flat/example-main.webp'}}
        rows,errors=m.parse(dataset(item('one'),e));self.assertEqual(2,len(rows));self.assertFalse(errors)
    def test_shared_images_are_distinct_ids(self):
        rows,errors=m.parse(dataset(item('one'),item('two')));self.assertEqual(2,len(rows));self.assertFalse(errors)
    def test_duplicate_ids_rejected(self):
        rows,errors=m.parse(dataset(item(),item()));self.assertEqual([],rows);self.assertEqual(2,len(errors))
    def test_unknown_optional_ignored_and_empty_muscles_allowed(self):
        e=item();e['future_field']={'anything':True};e['primary_muscles']=[]
        rows,errors=m.parse(dataset(e));self.assertEqual(1,len(rows));self.assertFalse(errors);self.assertEqual('_unspecified',rows[0]['equipment'])
    def test_invalid_enum_and_required_fields(self):
        for key,value in [('difficulty','safe-for-elderly'),('category','unknown'),('equipment','alien'),('name_en',''),('instructions_en',[])]:
            e=item();e[key]=value
            rows,errors=m.parse(dataset(e));self.assertFalse(rows);self.assertEqual(1,len(errors))
    def test_unsafe_asset_paths_and_remote_urls(self):
        for path in ['../secret.webp','https://example.org/a.webp','premium-samples/a.webp','images/flat/../../secret.webp','images/flat/a.webp?token=1']:
            e=item();e['images']={'flat':{'main':path}}
            self.assertEqual([],m.parse(dataset(e))[0])
    def test_missing_peak_rejected(self):
        e=item();del e['images']['flat']['peak'];self.assertEqual([],m.parse(dataset(e))[0])
    def test_schema_count_and_optional_type(self):
        with self.assertRaises(ValueError):m.parse(dict(schema_version=99,exercises=[]))
        with self.assertRaises(ValueError):m.parse(dict(schema_version=3,count=2,exercises=[]))
        e=item();e['tips_en']='not a list';self.assertEqual([],m.parse(dataset(e))[0])
if __name__=='__main__':unittest.main()
