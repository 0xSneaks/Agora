import {test} from 'node:test';
import assert from 'node:assert/strict';
import {DATA_KEY,KEYS_KEY,normalizeName,greeting,endpoint,modelIds,validateProvider,emptyData,validateData,readData,readKeys,writeData,writeKeys,publicBackup} from '../src/core.js';
const origin='https://sneaks.example';
const p={id:'p1',name:'Test',protocol:'openai',baseUrl:'https://api.example/v1',models:['model-1']};
const memory=()=>{const map=new Map();return {getItem:k=>map.get(k)??null,setItem:(k,v)=>map.set(k,v)};};
test('name is optional, trimmed and limited exactly like Android',()=>{
  assert.equal(normalizeName('   Sneaks   '),'Sneaks');assert.equal(normalizeName('x'.repeat(55)).length,40);assert.equal(normalizeName('   '),'');
});
test('all 24 local hours match Android greeting boundaries',()=>{
  for(let h=0;h<24;h++)assert.equal(greeting('Sneaks',h),`${h>=5&&h<=11?'Good morning':h>=12&&h<=16?'Good afternoon':h>=17&&h<=21?'Good evening':'Hey'}, Sneaks`);
  assert.equal(greeting('',8),'Welcome to Sneaks AI');
});
test('API URLs require external HTTPS, reject embedded secrets and query keys',()=>{
  assert.equal(endpoint('https://api.example/v1///',origin),'https://api.example/v1');
  for(const value of ['http://api.example','javascript:alert(1)','https://key@api.example','https://api.example?key=secret','https://api.example/#secret',origin+'/api','not-a-url'])assert.throws(()=>endpoint(value,origin));
});
test('provider model CRUD input preserves exact distinct IDs',()=>{
  assert.deepEqual(modelIds(' x\n\ny\nx '),['x','y']);
  assert.deepEqual(validateProvider(p,origin),p);
  assert.throws(()=>validateProvider({...p,protocol:'unknown'},origin));
  assert.throws(()=>validateProvider({...p,models:['model bad']},origin));
});
test('local history persists separately from keys and round-trips',()=>{
  const store=memory(), data=emptyData();data.providers=[p];data.name='Sneaks';
  writeData(store,data);writeKeys(store,{p1:'secret-sentinel'});
  assert.deepEqual(readData(store,origin),data);assert.deepEqual(readKeys(store),{p1:'secret-sentinel'});
  assert.ok(!store.getItem(DATA_KEY).includes('secret-sentinel'));assert.ok(store.getItem(KEYS_KEY).includes('secret-sentinel'));
});
test('blocked/full storage reports failure instead of silently losing data',()=>{
  const blocked={setItem(){throw new Error('quota');}};
  assert.throws(()=>writeData(blocked,emptyData()),/Could not save/);assert.throws(()=>writeKeys(blocked,{}),/Could not save/);
});
test('corrupt local data is preserved and not treated as a new install',()=>{
  const store=memory();store.setItem(DATA_KEY,'invalid');assert.throws(()=>readData(store,origin));assert.equal(store.getItem(DATA_KEY),'invalid');
  store.setItem(KEYS_KEY,'["bad"]');assert.throws(()=>readKeys(store));
});
test('backup is a field whitelist with no keys and imports valid local chats',()=>{
  const data={...emptyData(),apiKey:'secret-sentinel',providers:[{...p,key:'secret-sentinel'}],conversations:[{id:'c1',title:'hello',providerId:p.id,model:'model-1',system:'be helpful',createdAt:'2026-10-19',messages:[{role:'user',content:'hello',key:'secret-sentinel'}]}]};
  const backup=publicBackup(data);assert.ok(!JSON.stringify(backup).includes('secret-sentinel'));assert.equal(validateData(backup,origin).conversations[0].messages[0].content,'hello');
});
test('imports reject duplicate IDs, malformed messages and unsafe endpoints',()=>{
  assert.throws(()=>validateData({...emptyData(),providers:[p,p]},origin));
  assert.throws(()=>validateData({...emptyData(),providers:[{...p,baseUrl:origin}]},origin));
  assert.throws(()=>validateData({...emptyData(),conversations:[{id:'c',title:'x',providerId:'p',model:'m',messages:[{role:'system',content:'x'}]}]},origin));
});
