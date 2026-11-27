"""检查 Gradle 的 JUnit XML，拒绝没有任何实际执行用例的模块。"""
from pathlib import Path
import xml.etree.ElementTree as ET

required = {
    'fun.bm.mili.regression.PathRecomputationTestSuite',
    'fun.bm.mili.regression.CommandTreeEnumerationTestSuite',
    'fun.bm.mili.regression.CreativeRemovalTestSuite',
    'fun.bm.mili.update.MiliAutoUpdateTestSuite',
    'net.minecraft.world.level.block.CactusRotationTest',
    'net.minecraft.world.level.block.CarpetLargeBarrelTest',
    'net.minecraft.core.dispenser.CarpetDispenserTest',
    'org.leavesmc.leaves.plugin.ServerFeatureManagerTest',
    'org.leavesmc.leaves.protocol.servux.litematics.LitematicaSchematicTest',
}
for module in ('mili-api', 'mili-server'):
    executed = set()
    counts = dict.fromkeys(('tests', 'failures', 'errors', 'skipped'), 0)
    for report in Path(module, 'build/test-results/test').glob('TEST-*.xml'):
        suite = ET.parse(report).getroot()
        executed.update(tc.get('classname') for tc in suite.iter('testcase') if tc.find('skipped') is None)
        for key in counts:
            counts[key] += int(suite.get(key, 0))
    if module == 'mili-server' and required - executed:
        raise SystemExit(f'未执行本轮发布要求的测试类：{sorted(required - executed)}')
    print(f'{module}: {counts}')
    if counts['tests'] <= counts['skipped'] or counts['failures'] or counts['errors']:
        raise SystemExit(f'{module} 没有执行测试或有测试失败')
