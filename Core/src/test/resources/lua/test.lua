function printTable(table, initialContents) 
  if (initialContents) then
    print(initialContents)
  end
  
  for k, v in pairs(table) do
    print(k,v)
  end
end

printTable(_G, "Globals: ")
print("Version: ".._VERSION)
print("API Version: ".._API_VERSION)

local testJson = {test1="Hello", test2="World"}
printTable(testJson, "Original Table:")
local jsonString = json.toJsonString(testJson)
print("Parsed json string: \n"..jsonString)
local convertedJson = json.parseJsonString(jsonString)
printTable(convertedJson, "Converted Json table:")
