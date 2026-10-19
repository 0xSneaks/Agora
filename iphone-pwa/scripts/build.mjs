import {cp,readFile,writeFile,mkdir,rm} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {join,dirname} from 'node:path';
const root=dirname(dirname(fileURLToPath(import.meta.url)));
await rm(join(root,'dist'),{recursive:true,force:true});
await mkdir(join(root,'dist'),{recursive:true});
await cp(join(root,'public'),join(root,'dist'),{recursive:true});
await cp(join(root,'src'),join(root,'dist/src'),{recursive:true});
await cp(join(root,'LICENSE'),join(root,'dist/LICENSE.txt'));
await cp(join(root,'README.md'),join(root,'dist/SOURCE.txt'));
const hash=createHash('sha256');
for(const path of ['src/app.js','src/core.js','src/providers.js','public/index.html','public/style.css','public/sw.js','public/manifest.webmanifest','README.md']) hash.update(await readFile(join(root,path)));
const revision=hash.digest('hex').slice(0,16);
await writeFile(join(root,'dist/sw.js'),(await readFile(join(root,'public/sw.js'),'utf8')).replace('__REVISION__',revision));
for(const size of [192,512]) {
  const icon=await readFile(join(root,`dist/icons/icon-${size}.png`));
  if(icon.readUInt32BE(16)!==size || icon.readUInt32BE(20)!==size) throw new Error('Invalid icon dimensions');
}
console.log(`Built static PWA, revision ${revision}. No server functions or dependencies.`);
