"""检查 Gradle 的 JUnit XML，拒绝没有任何实际执行用例的模块。"""
from pathlib import Path
import xml.etree.ElementTree as ET

for module in ('mili-api', 'mili-server'):
    counts = dict.fromkeys(('tests', 'failures', 'errors', 'skipped'), 0)
    for report in Path(module, 'build/test-results/test').glob('TEST-*.xml'):
        suite = ET.parse(report).getroot()
        for key in counts:
            counts[key] += int(suite.get(key, 0))
    print(f'{module}: {counts}')
    if counts['tests'] <= counts['skipped'] or counts['failures'] or counts['errors']:
        raise SystemExit(f'{module} 没有执行测试或有测试失败')
